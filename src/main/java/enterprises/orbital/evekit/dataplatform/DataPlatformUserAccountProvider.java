package enterprises.orbital.evekit.dataplatform;

import enterprises.orbital.base.OrbitalProperties;
import enterprises.orbital.db.ConnectionFactory;
import enterprises.orbital.oauth.UserAccount;
import enterprises.orbital.oauth.UserAccountProvider;
import enterprises.orbital.oauth.UserAuthSource;

public class DataPlatformUserAccountProvider implements UserAccountProvider {

  @Override
  public UserAccount getAccount(String uid) {
    long user_id = 0;
    try {
      user_id = Long.valueOf(uid);
    } catch (NumberFormatException e) {
      user_id = 0;
    }
    return DataPlatformUserAccount.getAccount(user_id);
  }

  @Override
  public UserAuthSource getSource(UserAccount acct, String source) {
    assert acct instanceof DataPlatformUserAccount;
    return DataPlatformUserAuthSource.getSource((DataPlatformUserAccount) acct, source);
  }

  @Override
  public void removeSourceIfExists(UserAccount acct, String source) {
    assert acct instanceof DataPlatformUserAccount;
    DataPlatformUserAuthSource.removeSourceIfExists((DataPlatformUserAccount) acct, source);
  }

  @Override
  public UserAuthSource getBySourceScreenname(String source, String screenName) {
    return DataPlatformUserAuthSource.getBySourceScreenname(source, screenName);
  }

  @Override
  public UserAuthSource createSource(UserAccount newUser, String source, String screenName, String body) {
    assert newUser instanceof DataPlatformUserAccount;
    return DataPlatformUserAuthSource.createSource((DataPlatformUserAccount) newUser, source, screenName, body);
  }

  @Override
  public UserAccount createNewUserAccount(boolean disabled) {
    return DataPlatformUserAccount.createNewUserAccount(false);
  }

}
