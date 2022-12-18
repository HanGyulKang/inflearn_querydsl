package study.querydsl.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.annotation.Rollback;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static study.querydsl.entity.QMember.member;

@SpringBootTest
@Transactional
class MemberRepositoryTest {

    @Autowired
    EntityManager em;

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    MemberQueryRepository memberQueryRepository;

    @Autowired
    MemberTestRepository memberTestRepository;

    @Test
    @Rollback
    public void basicTest() {
        Member member = new Member("member1", 10);
        memberRepository.save(member);

        Member findMember = memberRepository.findById(member.getId()).get();
        assertThat(findMember).isEqualTo(member);

        List<Member> result1 = memberRepository.findAll();
        assertThat(result1).containsExactly(member);

        List<Member> result2 = memberRepository.findByUsername("member1");
        assertThat(result2).containsExactly(member);
    }

    @Test
    public void searchTest() {
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

        MemberSearchCondition condition = new MemberSearchCondition();
        condition.setAgeGoe(35);
        condition.setAgeLoe(40);
        condition.setTeamName("teamB");

        List<MemberTeamDto> result2 = memberRepository.search(condition);
        assertThat(result2)
                .extracting("username")
                .containsExactly("member4");

        List<MemberTeamDto> result3 = memberQueryRepository.search(condition);
        assertThat(result3)
                .extracting("username")
                .containsExactly("member4");
    }

    @Test
    public void searchSimplePageTest() {
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

        MemberSearchCondition condition = new MemberSearchCondition();
        PageRequest pageRequest = PageRequest.of(0, 3);


        // content + total count
        Page<MemberTeamDto> result = memberRepository.searchPageSimple(condition, pageRequest);
        assertThat(result.getSize()).isEqualTo(3);
        assertThat(result.getContent())
                .extracting("username")
                .containsExactly("member1", "member2", "member3");

        // content, total count
        Page<MemberTeamDto> result1 = memberRepository.searchPageComplex(condition, pageRequest);
        assertThat(result1.getSize()).isEqualTo(3);
        assertThat(result1.getContent())
                .extracting("username")
                .containsExactly("member1", "member2", "member3");

        // QuerydslRepositorySupport custom test
        condition.setAgeGoe(10);
        condition.setAgeLoe(40);
        condition.setUsername("member1");
        condition.setTeamName("teamA");

        Page<Member> result2 = memberTestRepository.applyPaginationSimple(condition, pageRequest);

        List<Member> content = result2.getContent();
        System.out.println("content size : " + content.size());
        for(Member m : content) {
            System.out.println(m.toString());
        }

        assertThat(result2.getContent().size()).isEqualTo(1);
        assertThat(result2.getContent())
                .extracting("username")
                .containsExactly("member1");


        Page<Member> result3 = memberTestRepository.applyPaginationComplex(condition, pageRequest);
        assertThat(result3.getContent().size()).isEqualTo(1);
        assertThat(result3.getContent())
                .extracting("username")
                .containsExactly("member1");
    }

    @Test
    public void querydslPredicateExecutorTest() {
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

        QMember member = QMember.member;

        // 아주 편리한 기능이지만 Join관계가 있는 환경에서는 사용하기가 매우 까다로워서 실제 운영 환경에서는 사용을 권하지 않는다.
        // Join이 불가능 함.
        // 복잡한 실무 환경에서는 사용 한계가 명확하다.
        Iterable<Member> members = memberRepository
                .findAll(member.age.between(10, 40).and(member.username.eq("member1")));

        for(Member findMember : members) {
            System.out.println("member1 = " + findMember.toString());
        }
    }

    @Test
    void findByUsername() {
    }
}