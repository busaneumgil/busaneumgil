package com.ssafy.e102.domain.route.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ssafy.e102.domain.route.entity.OdsayLoadLane;

public interface OdsayLoadLaneRepository extends JpaRepository<OdsayLoadLane, Long> {

	List<OdsayLoadLane> findAllByMapObjIn(Collection<String> mapObjs);

	Optional<OdsayLoadLane> findByMapObj(String mapObj);
}
