package com.example.heartbit.acceptance.asset;


import com.example.heartbit.repository.InvestRepository;
import com.example.heartbit.service.InvestService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
public class InvestAcceptanceTest {



    @Autowired
    private InvestRepository investRepository;

//    @Test
//    @DisplayName("특정 코인을 매수하면 보유 자산 목록에 추가된다.")
//    void investCoinTest() {
//
//    }


    
}
