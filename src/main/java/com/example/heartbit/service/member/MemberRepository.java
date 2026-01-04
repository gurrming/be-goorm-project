package com.example.heartbit.service.member;

import com.example.heartbit.domain.Member;
import org.springframework.data.repository.Repository;

interface MemberRepository extends Repository<Member, Long> {
}
