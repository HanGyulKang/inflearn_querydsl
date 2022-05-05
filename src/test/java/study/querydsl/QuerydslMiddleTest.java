package study.querydsl;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
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
}
