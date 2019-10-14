package com.kdf.web.server.repository;

import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.kdf.web.server.bean.PvAll;

@Repository
public interface PvAllRepository extends JpaRepository<PvAll, Integer> {

	List<PvAll> findAllByAppidAndRequestTimeBetween(String appid, Date startTime, Date endTime);
	
	@Query(value="select count(1) as cnt, date_format(request_time, '%Y-%m-%d %h') as time from kfd_pv_all where appid = ? group by date_format(request_time, '%Y-%m-%d %h')",nativeQuery = true)
	List<Map<String, Object>> findAllPvMap(String appid);
	
}
