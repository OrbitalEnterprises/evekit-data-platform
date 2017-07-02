package enterprises.orbital.evekit.dataplatform;

import enterprises.orbital.base.OrbitalProperties;
import enterprises.orbital.db.ConnectionFactory;

public class DataPlatformProvider  {
  public static final String DATA_PLATFORM_PU_PROP    = "enterprises.orbital.evekit.dataplatform.properties.persistence_unit";
  public static final String DATA_PLATFORM_PU_DEFAULT = "data-platform-registry-properties";
  public static final String PROP_EVE_TOKEN_CLIENT_ID = "enterprises.orbital.token.eve_client_id";
  public static final String PROP_EVE_TOKEN_SECRET_KEY = "enterprises.orbital.token.eve_secret_key";

  public static ConnectionFactory getFactory() {
    return ConnectionFactory.getFactory(OrbitalProperties.getGlobalProperty(DATA_PLATFORM_PU_PROP,
                                                                            DATA_PLATFORM_PU_DEFAULT));
  }

}
