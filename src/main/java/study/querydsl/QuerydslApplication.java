package study.querydsl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import javax.persistence.EntityManager;

@SpringBootApplication
public class QuerydslApplication {

	public static void main(String[] args) {
		SpringApplication.run(QuerydslApplication.class, args);
	}

	// JPAQueryFactory 의 동시성 문제는 EntityManager 에 의존
	// 스프링에서 EntityManager 가 싱글톤이지만, 프록시를 주입해줘서 트랜잭션 단위로 각각 바인딩되도록 라우팅해줌
	// 책 13.1 참고하기
	@Bean
	public JPAQueryFactory jpaQueryFactory(EntityManager em) {
		return new JPAQueryFactory(em);
	}
}
