package com.example.heartbit.service;


import com.example.heartbit.dto.invest.InvestPortfolioDto;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class InvestService {
    public InvestPortfolioDto getPortfolio() {


        return new InvestPortfolioDto(BigDecimal.ZERO);
    }
}
