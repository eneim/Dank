package me.saket.dank.di;

import android.app.Application;

import com.danikula.videocache.HttpProxyCacheServer;
import com.squareup.moshi.Moshi;
import com.squareup.sqlbrite.BriteDatabase;

import me.saket.dank.data.DankRedditClient;
import me.saket.dank.data.ErrorManager;
import me.saket.dank.data.InboxManager;
import me.saket.dank.data.SharedPrefsManager;
import me.saket.dank.data.SubmissionManager;
import me.saket.dank.data.SubredditSubscriptionManager;
import me.saket.dank.data.UserPrefsManager;
import me.saket.dank.data.VotingManager;
import me.saket.dank.notifs.MessagesNotificationManager;
import me.saket.dank.ui.submission.CommentsManager;
import me.saket.dank.ui.user.UserSession;
import me.saket.dank.utils.ImgurRepository;
import me.saket.dank.utils.JacksonHelper;

public class Dank {
  private static DankAppComponent appComponent;

  public static void initDependencies(Application application) {
    appComponent = DaggerDankAppComponent.builder()
        .dankAppModule(new DankAppModule(application))
        .build();
  }

  public static DankAppComponent dependencyInjector() {
    return appComponent;
  }

  public static DankRedditClient reddit() {
    return appComponent.dankRedditClient();
  }

  public static SharedPrefsManager sharedPrefs() {
    return appComponent.sharedPrefs();
  }

  public static UserPrefsManager userPrefs() {
    return appComponent.userPrefs();
  }

  public static HttpProxyCacheServer httpProxyCacheServer() {
    return appComponent.httpProxyCacheServer();
  }

  public static DankApi api() {
    return appComponent.api();
  }

  public static ImgurRepository imgur() {
    return appComponent.imgur();
  }

  public static SubredditSubscriptionManager subscriptions() {
    return appComponent.subredditSubscriptionManager();
  }

  public static JacksonHelper jackson() {
    return appComponent.jacksonHelper();
  }

  public static ErrorManager errors() {
    return appComponent.errorManager();
  }

  public static InboxManager inbox() {
    return appComponent.inboxManager();
  }

  public static MessagesNotificationManager messagesNotifManager() {
    return appComponent.messagesNotificationManager();
  }

  public static SubmissionManager submissions() {
    return appComponent.submissionManager();
  }

  public static Moshi moshi() {
    return appComponent.moshi();
  }

  public static BriteDatabase database() {
    return appComponent.briteDatabase();
  }

  public static VotingManager voting() {
    return appComponent.votingManager();
  }

  public static UserSession userSession() {
    return appComponent.userSession();
  }

  public static CommentsManager comments() {
    return appComponent.commentsManager();
  }
}
