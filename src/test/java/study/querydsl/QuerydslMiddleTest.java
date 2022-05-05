package study.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.dto.UserDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;

import java.util.List;

import static com.querydsl.jpa.JPAExpressions.*;
import static org.assertj.core.api.Assertions.*;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

@SpringBootTest
@Transactional
public class QuerydslMiddleTest {

    @Autowired
    EntityManager em;

    JPAQueryFactory jpaQueryFactory;

    @BeforeEach
    public void before() {
        jpaQueryFactory = new JPAQueryFactory(em);

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
    }

    /** ========================================================================
     * 1. Projection
     ======================================================================== */
    @Test
    public void simpleProjection() {
        List<String> result = jpaQueryFactory
                .select(member.username)
                .from(member)
                .fetch();

        System.out.println(result.toString());
    }

    @Test
    public void tupleProjection() {
        List<Tuple> result = jpaQueryFactory
                .select(member.username,
                        member.age)
                .from(member)
                .fetch();

        for(Tuple tuple : result) {
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);
            System.out.println(username);
            System.out.println(age);
        }
    }

    @Test
    public void findDtoByJPQL() {
        List<MemberDto> resultList = em.createQuery(
                "select " +
                        "new study.querydsl.dto.MemberDto(m.username, m.age)" +
                        " from Member m"
                        , MemberDto.class)
                .getResultList();

        for(MemberDto m : resultList) {
            System.out.println(m);
        }
    }

    /**
     * DTO 프로퍼티(setter)에 접근해서 조회
     */
    @Test
    public void findDtoSetter() {
        List<MemberDto> result = jpaQueryFactory
                .select(Projections.bean(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        System.out.println(result.toString());
    }

    /**
     * DTO 필드에 접근해서 조회(Getter, Setter필요 없음)
     */
    @Test
    public void findDtoField() {
        List<MemberDto> result = jpaQueryFactory
                .select(Projections.fields(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for(MemberDto m : result) {
            System.out.println(m);
        }
    }
    /**
     * 생성자를 이용한 조회
     */
    @Test
    public void findDtoConstuctor() {
        List<MemberDto> result = jpaQueryFactory
                .select(Projections.constructor(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for(MemberDto m : result) {
            System.out.println(m);
        }
    }
    @Test
    public void findUserDto() {
        QMember memberSub = new QMember("memberSub");

        List<UserDto> result = jpaQueryFactory
                .select(Projections.fields(UserDto.class,
                        member.username.as("name"),
                        ExpressionUtils.as(
                                        select(memberSub.age.max())
                                        .from(memberSub)
                                , "age")
                ))
                .from(member)
                .fetch();

        for(UserDto m : result) {
            System.out.println(m);
        }
    }

    @Test
    public void findDtoBuQueryProjection() {
        List<MemberDto> result = jpaQueryFactory
                .select(new QMemberDto(
                        member.username,
                        member.age
                ))
                .from(member)
                .fetch();

        for(MemberDto m : result) {
            System.out.println(m);
        }
    }


    /** ========================================================================
     * 2. 동적 쿼리
     ======================================================================== */
    @Test
    public void dynamicQuery() {
        String usernameParam = "member1";
        Integer ageParam = 10;

        // BooleanBuilder 사용
        List<Member> resultA = searchMemberWithBooleanBuilder(usernameParam, ageParam);
        // where절 다중 파라미터
        List<MemberDto> resultB = searchMemberWithMultiWhere(usernameParam, ageParam);
        assertThat(resultA.size()).isEqualTo(1);
        assertThat(resultB.size()).isEqualTo(1);

    }

    private List<Member> searchMemberWithBooleanBuilder(String usernameCond, Integer ageCond) {
        // 초기값 지정 가능
        // BooleanBuilder builder = new BooleanBuilder(member.username.eq(usernameCond));
        BooleanBuilder builder = new BooleanBuilder();

        if(usernameCond != null) {
            builder.and(member.username.eq(usernameCond));
        }

        if(ageCond != null) {
            builder.and(member.age.eq(ageCond));
        }

        return jpaQueryFactory
                .selectFrom(member)
                .where(builder)
                .fetch();
    }
    private List<MemberDto> searchMemberWithMultiWhere(String usernameCond , Integer ageCond) {
        return jpaQueryFactory
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .where(usernameEq(usernameCond), ageEq(ageCond))
                //.where(allEq(usernameCond, ageCond))
                .fetch();
    }

    private BooleanExpression usernameEq(String usernameCond) {
        return usernameCond == null ? null : member.username.eq(usernameCond);
    }
    private BooleanExpression ageEq(Integer ageCond) {
        return ageCond == null ? null : member.age.eq(ageCond);
    }
    private BooleanExpression allEq(String usernameCond, Integer ageCond) {
        return usernameEq(usernameCond).and(ageEq(ageCond));
    }


    /** ========================================================================
     * 3. 수정, 삭제 배치 쿼리
     ======================================================================== */
    /**
     * 대량 데이터 처리 (벌크 업데이트)
     */
    @Test
    //@Commit
    public void bulkUpdate() {
        // count : 영향을 받은 건수
        // 1. 영속성 컨텍스트를 무시하고 실행되기 때문에 데이터베이스와 영속성 컨텍스트의 값이 다름
        long count = jpaQueryFactory
                .update(member)
                .set(member.username, "비회원")
                .where(member.age.lt(28))
                .execute();

        // 3. 우선권에 대한 문제 해결 : EntityManager 초기화
        /**
         * 매우 중요
         */
        em.flush();
        em.clear();

        // 2. 우선권 : 영속성 컨텍스트 > 데이터베이스
        List<Member> result = jpaQueryFactory
                .selectFrom(member)
                .fetch();

        for(Member m : result) {
            System.out.println(m);
        }
    }

    @Test
    public void bulkAdd() {
        jpaQueryFactory
                .update(member)
                // 더 하기
                .set(member.age, member.age.add(1))
                // 빼기
                //.set(member.age, member.age.add(-1))
                // 곱하기
                // .set(member.age, member.age.multiply(2))
                .execute();

        em.flush();
        em.clear();
    }

    @Test
    public void bulkDelete() {
        long count = jpaQueryFactory
                .delete(member)
                .where(member.age.goe(18))
                .execute();

        em.flush();
        em.clear();

        List<Member> result = jpaQueryFactory
                .selectFrom(member)
                .fetch();

        for(Member m : result) {
            System.out.println(m);
        }
    }


    /** ========================================================================
     * 4. SQL function 호출
     ======================================================================== */
    @Test
    public void sqlFunction1() {
        List<String> result = jpaQueryFactory
                .select(Expressions.stringTemplate(
                        "function('replace', {0}, {1}, {2})",
                        member.username, "member", "M")
                )
                .from(member)
                .fetch();

        for(String s : result) {
            System.out.println(s);
        }
    }

    @Test
    public void sqlFunction2() {
        List<String> result = jpaQueryFactory
                .select(member.username)
                .from(member)
//                .where(member.username.eq(
//                        Expressions.stringTemplate(
//                                "function('lower', {0})",
//                                member.username
//                        )
//                ))
                .where(member.username.eq(member.username.lower()))
                .fetch();

        for(String s : result) {
            System.out.println(s);
        }
    }

}
