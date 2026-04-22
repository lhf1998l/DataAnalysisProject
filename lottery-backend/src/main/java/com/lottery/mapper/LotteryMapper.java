package com.lottery.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lottery.entity.LotteryRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
@Mapper
public interface LotteryMapper extends BaseMapper<LotteryRecord> {
    int insertOrUpdate(@Param("record") LotteryRecord record);
}
