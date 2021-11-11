package study.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.dto.UserDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static study.querydsl.entity.QMember.member;

@SpringBootTest
@Transactional
public class QuerydslTest2 {

    @Autowired
    EntityManager em;

    JPAQueryFactory queryFactory;

    @BeforeEach
    public void testEntity() {
        queryFactory = new JPAQueryFactory(em); // em을 동시성 문제 없이 분배해줘서 문제없이 사용 가능.

        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);

        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);
        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);

        // 초기화
        em.flush(); // 영속성 컨텍스트에 있는 쿼리를 DB로 날림.
        em.clear(); // 영속성 컨텍스트 초기화. 캐싱 삭제.

        List<Member> members = em.createQuery("select m from Member m", Member.class)
                .getResultList();

        for (Member member : members) {
            System.out.println(member);
        }
    }

    // 프로젝션 - select 대상 지정 (타입 명확하게 지정 가능)
    @Test
    public void simpleProjection() {
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println(s);
        }
    }

    // Tuple 은 querydsl 에서 제공하기 때문에 repository 에서만 사용하기 (querydsl에 의존적이니까)
    @Test
    public void tupleProjection() {
        List<Tuple> result = queryFactory
                .select(member.username, member.age)
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);
            System.out.println("username = " + username);
            System.out.println("age = " + age);
        }
    }

    @Test
    public void findDtoByJPQL() {
        List<MemberDto> result = em.createQuery("select new study.querydsl.dto.MemberDto(m.username, m.age) from Member m", MemberDto.class)
                .getResultList();

        for (MemberDto memberDto : result) {
            System.out.println(memberDto);
        }
    }

    /**
     * Querydsl 빈 생성 방법
     * 1. 프로퍼티 접근
     * 2. 필드 직접 접근
     * 3. 생성자 사용
     */
     @Test
     public void findDtoBySetter() {
         List<MemberDto> result = queryFactory
                 .select(Projections.bean(MemberDto.class,
                         member.username,
                         member.age))    // MemberDto 기본 생성자로 객체 생성 후 setter 로 값을 세팅
                 .from(member)
                 .fetch();

         for (MemberDto memberDto : result) {
             System.out.println(memberDto);
         }
     }

     @Test
    public void findDtoByFiled() {
         List<MemberDto> result = queryFactory
                 .select(Projections.fields(MemberDto.class,
                         member.username,
                         member.age))
                 .from(member)
                 .fetch();

         for (MemberDto memberDto : result) {
             System.out.println(memberDto);
         }
     }

    @Test
    public void findDtoByConstructor() {
        List<MemberDto> result = queryFactory
                .select(Projections.constructor(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println(memberDto);
        }
    }

    @Test
    public void findUserDto() {
        QMember memberSub = new QMember("memberSub");

        List<UserDto> result = queryFactory
                .select(Projections.fields(UserDto.class,
                        member.username.as("name"),             // Dto 와 이름이 다를 때
                        ExpressionUtils.as(JPAExpressions       // 필드나 서브쿼리 별칭에 사용
                                .select(memberSub.age.max())
                                .from(memberSub), "age")
                ))
                .from(member)
                .fetch();

        for (UserDto userDto : result) {
            System.out.println(userDto);
        }
    }

    @Test
    public void findUserDtoConstructor() {
        List<UserDto> result = queryFactory
                .select(Projections.constructor(UserDto.class, // 생성자 방식은 필드 타입이 맞지 않을 경우 runtime 에 예외 확인 가능
                        member.username,
                        member.age
                ))
                .from(member)
                .fetch();

        for (UserDto userDto : result) {
            System.out.println(userDto);
        }
    }

    /**
     * 프로젝션과 결과 반환 @QueryProjection
     * Dto에 Querydsl 의존성이 묶이게 됨...
     */
    @Test
    public void findDtoByQueryProjection() {
        List<MemberDto> result = queryFactory
                .select(new QMemberDto(member.username, member.age))    // 컴파일 시에 타입 확인 가능
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println(memberDto);
        }
    }

    /**
     * 동적 쿼리
     * 1. BooleanBuilder
     * 2. Where 다중 파라미터 사용
     */
    @Test
    public void dynamicQuery_BooleanBuilder() {
        String usernameParam = "member1";
        Integer ageParam = null;

        List<Member> result = searchMember1(usernameParam, ageParam);

        assertThat(result.size()).isEqualTo(1);
        assertThat(result.get(0).getUsername()).isEqualTo(usernameParam);
    }

    private List<Member> searchMember1(String usernameParam, Integer ageParam) {

        BooleanBuilder builder = new BooleanBuilder();
        if (usernameParam != null) {
            builder.and(member.username.eq(usernameParam));
        }

        if (ageParam != null) {
            builder.and(member.age.eq(ageParam));
        }

        return queryFactory
                .selectFrom(member)
                .where(builder)
                .fetch();
    }

    // TODO 실무에서 유용!!! where 조건에 null 값은 무시된다!
    @Test
    public void dynamicQuery_WhereParam() {
        String usernameParam = "member1";
        Integer ageParam = 10;

        List<Member> result = searchMember2(usernameParam, ageParam);

        assertThat(result.size()).isEqualTo(1);
        assertThat(result.get(0).getUsername()).isEqualTo(usernameParam);
        assertThat(result.get(0).getAge()).isEqualTo(ageParam);
    }

    private List<Member> searchMember2(String usernameParam, Integer ageParam) {
        return queryFactory
                .selectFrom(member)
                .where(usernameEq(usernameParam), ageEq(ageParam))
                .fetch();
    }

    private BooleanExpression usernameEq(String usernameParam) {
        if (usernameParam == null) {
            return null;
        }
        return member.username.eq(usernameParam);
    }

    private BooleanExpression ageEq(Integer ageParam) {
        return ageParam != null ? member.age.eq(ageParam) : null;
    }

    private Predicate allEq(String usernameCond, Integer ageCond) {
        // null 처리 필요
        return usernameEq(usernameCond).and(ageEq(ageCond));
    }

    /**
     * 수정, 삭제 배치 쿼리
     */
    // 쿼리 한번으로 대량 데이터 수정
    @Test
    public void bulkUpdate() {
        long count = queryFactory                   // 영향을 받은 로우 수
                .update(member)
                .set(member.username, "비회원")
                .where(member.age.lt(28))
                .execute();

        assertThat(count).isEqualTo(2);

        // **** 벌크 연산은 바로 쿼리 실행되어서(영속성 컨텍스트 무시) 영속성 컨텍스트와 상태가 안맞음
        List<Member> result = queryFactory      // <= DB에서 값을 가져와도 영속성 컨텍스트에 있어서 DB에서 가져온 값 버림
                .selectFrom(member)
                .fetch();

        // 그래서 초기화 하자!!!
        em.flush();
        em.clear();

        List<Member> result2 = queryFactory
                .selectFrom(member)
                .fetch();

        for (Member m : result2) {
            System.out.println(m);
        }
    }

    @Test
    public void bulkAdd() {
        long count = queryFactory
                .update(member)
                .set(member.age, member.age.add(1))
                .execute();
    }

    @Test
    public void bulkDelete() {
        queryFactory
                .delete(member)
                .where(member.age.gt(18))
                .execute();
    }

    /**
     * SQL function 호출하기
     */
    @Test
    public void sqlFunction() {
        List<String> result = queryFactory
                .select(Expressions.stringTemplate("function('replace', {0}, {1}, {2})", member.username, "member", "M"))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println(s);
        }
    }

    @Test
    public void sqlFunction2() {
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
                .where(member.username.eq(member.username.lower()))
                .fetch();

        for (String s : result) {
            System.out.println(s);
        }
    }
}
