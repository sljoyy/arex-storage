package com.arextest.storage.service;

import com.arextest.model.mock.AREXMocker;
import com.arextest.model.mock.MergeRecordDTO;
import com.arextest.model.mock.MockCategoryType;
import com.arextest.model.mock.Mocker;
import com.arextest.storage.repository.RepositoryProvider;
import com.arextest.storage.repository.RepositoryProviderFactory;
import com.arextest.storage.utils.JsonUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.Date;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MockSourceEditionService {

  private final RepositoryProviderFactory providerFactory;
  private final ScheduleReplayingService scheduleReplayingService;

  public MockSourceEditionService(RepositoryProviderFactory providerFactory,
      ScheduleReplayingService scheduleReplayingService,
      Set<MockCategoryType> entryPointTypes) {
    this.providerFactory = providerFactory;
    this.scheduleReplayingService = scheduleReplayingService;
  }

  public <T extends Mocker> boolean add(String providerName, T item) {
    RepositoryProvider<T> repositoryWriter = providerFactory.findProvider(providerName);
    return repositoryWriter != null && repositoryWriter.save(item);
  }

  public <T extends Mocker> T editMergedMocker(String providerName, AREXMocker item) {
    RepositoryProvider<T> repositoryProvider = providerFactory.findProvider(providerName);
    if (repositoryProvider == null) {
      LOGGER.warn("Could not found provider for {}", providerName);
      return null;
    }
    if (item.getId() == null) {
      LOGGER.warn("The id is empty");
      return null;
    }
    T mergedMocker = repositoryProvider.queryById(item.getCategoryType(), item.getId());
    mergedMocker.setCategoryType(item.getCategoryType());
    try {
      List<MergeRecordDTO> mergeRecordDTOS = new ArrayList<>(
          JsonUtil.OBJECT_MAPPER.readValue(mergedMocker.getTargetResponse().getBody(),
              new TypeReference<List<MergeRecordDTO>>() {
              }));
      MergeRecordDTO mergeRecordDTO = mergeRecordDTOS.get(item.getIndex());
      mergeRecordDTO.setRequest(item.getTargetRequest().getBody());
      mergeRecordDTO.setResponse(item.getTargetResponse().getBody());
      mergeRecordDTO.setUseMock(item.getUseMock());
      mergedMocker.getTargetResponse()
          .setBody(JsonUtil.OBJECT_MAPPER.writeValueAsString(mergeRecordDTOS));
    } catch (Exception e) {
      LOGGER.error("parse merge record error:{}", e.getMessage(), e);
    }
    return mergedMocker;
  }

  public <T extends Mocker> boolean update(String providerName, T item) {
    RepositoryProvider<T> repositoryWriter = providerFactory.findProvider(providerName);
    item.setUpdateTime(System.currentTimeMillis());
    return repositoryWriter != null && repositoryWriter.update(item);
  }

  public boolean removeAll(String providerName, String recordId) {
    if (StringUtils.isBlank(recordId)) {
      LOGGER.warn("The recordId is empty");
      return false;
    }
    RepositoryProvider<?> repositoryWriter = providerFactory.findProvider(providerName);
    if (repositoryWriter == null) {
      LOGGER.warn("Could not found provider for {}", providerName);
      return false;
    }
    Set<MockCategoryType> categoryTypes = providerFactory.getCategoryTypes();
    Map<String, Boolean> removeResults = new HashMap<>(categoryTypes.size());
    for (MockCategoryType categoryType : categoryTypes) {
      long removeResult = repositoryWriter.removeBy(categoryType, recordId);
      removeResults.put(categoryType.getName(), removeResult > 0);
    }
    LOGGER.info("remove all record result:{} for recordId:{}", removeResults, recordId);
    return true;
  }

  public boolean removeAllByAppId(String providerName, String appId) {
    if (StringUtils.isBlank(appId)) {
      LOGGER.warn("The appId is empty");
      return false;
    }
    RepositoryProvider<?> repositoryWriter = providerFactory.findProvider(providerName);
    if (repositoryWriter == null) {
      LOGGER.warn("Could not found provider for {}", providerName);
      return false;
    }
    Set<MockCategoryType> categoryTypes = providerFactory.getCategoryTypes();
    for (MockCategoryType categoryType : categoryTypes) {
      repositoryWriter.removeByAppId(categoryType, appId);
    }
    return true;
  }

  public boolean removeAllByOperationNameAndAppId(String providerName, String operationName,
      String appId) {
    RepositoryProvider<?> repositoryWriter = providerFactory.findProvider(providerName);
    if (StringUtils.isBlank(appId)) {
      LOGGER.warn("The appId is empty");
      return false;
    }
    if (repositoryWriter == null) {
      LOGGER.warn("Could not found provider for {}", providerName);
      return false;
    }
    Set<MockCategoryType> categoryTypes = providerFactory.getCategoryTypes();
    for (MockCategoryType categoryType : categoryTypes) {
      repositoryWriter.removeByOperationNameAndAppId(categoryType, operationName, appId);
    }
    return true;
  }

  public boolean remove(String providerName, String categoryName, String recordId, String id) {
    try {
      RepositoryProvider<?> repositoryWriter = providerFactory.findProvider(providerName);
      if (repositoryWriter == null) {
        LOGGER.warn("Could not found provider for {}", providerName);
        return false;
      }
      MockCategoryType categoryType = providerFactory.findCategory(categoryName);
      if (categoryType == null) {
        LOGGER.warn(
            "Could not found category for {}, did you customize a new category? try register it" +
                " in " +
                "config file",
            categoryName);
        return false;
      }
      if (StringUtils.isEmpty(id)) {
        return repositoryWriter.removeBy(categoryType, recordId) > 0;
      }
      return repositoryWriter.removeById(categoryType, id) > 0;
    } catch (Throwable throwable) {
      LOGGER.error("remove record error:{} from {} for category:{} at recordId:{} id:{}",
          throwable.getMessage(),
          providerName, categoryName,
          recordId,
          id,
          throwable);
      return false;
    }
  }

  public int copyTo(String srcProviderName, String srcRecordId, String targetProviderName,
      String targetRecordId) {
    int count = 0;
    if (StringUtils.equals(srcProviderName, targetProviderName)) {
      return count;
    }
    RepositoryProvider<Mocker> srcProvider = providerFactory.findProvider(srcProviderName);
    RepositoryProvider<Mocker> targetProvider = providerFactory.findProvider(targetProviderName);
    if (srcProvider == null || targetProvider == null) {
      LOGGER.warn("could not found provider for {} or {}", srcProvider, targetProvider);
      return count;
    }
    List<AREXMocker> srcMockers;
    Set<MockCategoryType> categoryTypes = providerFactory.getCategoryTypes();
    for (MockCategoryType categoryType : categoryTypes) {
      srcMockers = scheduleReplayingService.queryRecordList(srcProvider, categoryType, srcRecordId);
      if (CollectionUtils.isEmpty(srcMockers)) {
        continue;
      }
      List<Mocker> targetList = createTargetList(srcMockers, targetRecordId);
      if (CollectionUtils.isNotEmpty(targetList)) {
        if (targetProvider.saveList(targetList)) {
          count += targetList.size();
        }
      }
    }
    return count;
  }

  public int moveTo(String srcProviderName, String srcRecordId, String targetProviderName) {
    int movedCount = copyTo(srcProviderName, srcRecordId, targetProviderName, srcRecordId);
    if (movedCount != 0) {
      removeByRecordId(srcProviderName, srcRecordId);
    }
    return movedCount;
  }

  public boolean removeByRecordId(String providerName, String recordId) {
    RepositoryProvider<?> repositoryWriter = providerFactory.findProvider(providerName);
    if (repositoryWriter == null) {
      LOGGER.warn("Could not found provider for {}", providerName);
      return false;
    }
    long deleteCount = 0;
    for (MockCategoryType categoryType : providerFactory.getCategoryTypes()) {
      deleteCount += repositoryWriter.removeBy(categoryType, recordId);
    }
    LOGGER.info("removeByRecordId deleted {} mockers for recordId: {}", deleteCount, recordId);
    return true;
  }

  public boolean extendMockerExpirationByRecordId(String providerName, String recordId, Long extensionDays) {
    RepositoryProvider<?> repositoryWriter = providerFactory.findProvider(providerName);
    if (repositoryWriter == null) {
      LOGGER.warn("Could not found provider for {}", providerName);
      return false;
    }
    long updateCount = 0;
    Date expireDate = Date.from(LocalDateTime.now().plusDays(extensionDays).atZone(
        ZoneId.systemDefault()).toInstant());

    for (MockCategoryType categoryType : providerFactory.getCategoryTypes()) {
      updateCount += repositoryWriter.extendExpirationTo(categoryType, recordId,
          expireDate);
    }
    LOGGER.info("extendMockerExpirationByRecordId updated {} mockers for recordId: {}",
        updateCount, recordId);
    return updateCount > 0;
  }

  private List<Mocker> createTargetList(List<AREXMocker> srcMockers, String targetRecordId) {
    List<Mocker> targetList = null;
    for (Mocker mocker: srcMockers) {
      if (targetList == null) {
        targetList = new LinkedList<>();
      }
      mocker.setRecordId(targetRecordId);
      mocker.setId(null);
      mocker.setUpdateTime(System.currentTimeMillis());
      targetList.add(mocker);
    }
    return targetList;
  }
}
