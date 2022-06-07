package hello.core.singleton.member;

public interface MemberRepository {

    void save(Member member);

    Member findById(Long memberId);
}
