package com.example.heartbit.service;

import java.math.BigDecimal;

public record PriceChangedEvent(Long categoryId, BigDecimal newPrice) {}