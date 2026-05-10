package com.ssafy.e102.domain.admin.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.ssafy.e102.domain.admin.entity.AdminArea;

public interface AdminAreaRepository extends JpaRepository<AdminArea, Long> {

	@Query("""
		select distinct adminArea.gu, adminArea.dong
		from AdminArea adminArea
		order by adminArea.gu asc, adminArea.dong asc
		""")
	List<Object[]> findDistinctAreas();
}
