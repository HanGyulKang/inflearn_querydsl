package study.querydsl.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import study.querydsl.entity.Member;

import java.util.List;

public interface MemberRepository extends JpaRepository<Member, Long>, MemberRepositoryCustom {
    // select m from Member m where m.username = ?
    List<Member> findByUsername(String username);

    /**
     * MemberRepositoryCustom를 받아서 구현체에 있는 QueryDSL로 개발한 Repository의 Method를 호출할 수 있다.
     */
}
