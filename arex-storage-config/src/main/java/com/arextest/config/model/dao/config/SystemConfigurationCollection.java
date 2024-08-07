package com.arextest.config.model.dao.config;

import com.arextest.config.model.dao.BaseEntity;
import com.arextest.config.model.dto.system.ComparePluginInfo;
import com.arextest.config.model.dto.system.DesensitizationJar;
import java.util.Map;
import java.util.Set;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * @author wildeslam.
 * @create 2024/2/21 19:43
 */
@Data
@FieldNameConstants
@Document(SystemConfigurationCollection.DOCUMENT_NAME)
public class SystemConfigurationCollection extends BaseEntity {

  public static final String DOCUMENT_NAME = "SystemConfiguration";

  /**
   * The problem of prohibiting concurrent repeated insertions, the key is unique the function of
   * this record
   */
  private String key;
  private Map<String, Integer> refreshTaskMark;
  private DesensitizationJar desensitizationJar;
  private String callbackUrl;
  private Boolean authSwitch;
  private ComparePluginInfo comparePluginInfo;
  private String jwtSeed;
  private Set<String> ignoreNodeSet;


  public interface KeySummary {
    String REFRESH_DATA = "refresh_data";
    String DESERIALIZATION_JAR = "deserialization_jar";
    String CALLBACK_URL = "callback_url";
    String AUTH_SWITCH = "auth_switch";
    String COMPARE_PLUGIN_INFO = "compare_plugin_info";
    String JWT_SEED = "jwt_seed";
    String IGNORE_NODE_SET = "ignore_node_set";
  }
}
