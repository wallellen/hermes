package com.jlfex.hermes.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.PagingAndSortingRepository;

import com.jlfex.hermes.model.ImageManage;

public interface ImageManageRepository extends PagingAndSortingRepository<ImageManage, String>, JpaSpecificationExecutor<ImageManage> {
	ImageManage findOneByCode(String code);

	List<ImageManage> findOneByType(String type);
}
