package com.arextest.config.utils;

import com.arextest.config.model.dao.BaseEntity;
import com.mongodb.client.model.Updates;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.bson.conversions.Bson;
import org.springframework.data.mongodb.core.query.Update;

@Slf4j
public class MongoHelper {

  public static Bson getUpdate() {
    return Updates.combine(
        Updates.set(BaseEntity.Fields.dataChangeUpdateTime, System.currentTimeMillis()),
        Updates.setOnInsert(BaseEntity.Fields.dataChangeCreateTime, System.currentTimeMillis()));
  }

  public static void withMongoTemplateBaseUpdate(Update update) {
    update.set(BaseEntity.Fields.dataChangeUpdateTime, System.currentTimeMillis());
    update.setOnInsert(BaseEntity.Fields.dataChangeCreateTime, System.currentTimeMillis());
  }

  public static Update getFullTemplateUpdates(Object obj) {
    Map<String, Field> allFields = getAllField(obj);
    return getMongoTemplateUpdates(obj, allFields.keySet().toArray(new String[0]));
  }

  public static Update getMongoTemplateUpdates(Object obj, String... fieldNames) {
    Update update = new Update();
    Map<String, Field> allField = getAllField(obj);
    for (String fieldName : fieldNames) {
      try {
        if (allField.containsKey(fieldName)) {
          Field declaredField = allField.get(fieldName);
          declaredField.setAccessible(true);
          Object targetObj = declaredField.get(obj);
          if (targetObj != null) {
            update.set(fieldName, targetObj);
          }
        }
      } catch (IllegalAccessException e) {
        LOGGER.error(String.format("Class:[%s]. failed to get field %s", obj.getClass().getName(),
                fieldName),
            e);
      }
    }
    return update;
  }

  private static Map<String, Field> getAllField(Object bean) {
    Class<?> clazz = bean.getClass();
    Map<String, Field> fieldMap = new HashMap<>();
    while (clazz != null) {
      for (Field field : clazz.getDeclaredFields()) {
        // ignore static and synthetic field such as $jacocoData
        if (field.isSynthetic()) {
          continue;
        }
        if (!fieldMap.containsKey(field.getName())) {
          fieldMap.put(field.getName(), field);
        }
      }
      clazz = clazz.getSuperclass();
    }
    return fieldMap;
  }
}
