package com.mtravel.platform.common;

import java.util.List;

public record PageResult<T>(List<T> items, long total) {
}

