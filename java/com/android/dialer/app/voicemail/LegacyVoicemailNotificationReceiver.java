/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.android.dialer.app.voicemail;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build.VERSION_CODES;
import android.support.v4.os.BuildCompat;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.TelephonyManager;
import com.android.dialer.app.calllog.DefaultVoicemailNotifier;
import com.android.dialer.common.Assert;
import com.android.dialer.common.LogUtil;
import com.android.voicemail.VoicemailComponent;

/**
 * Receives {@link TelephonyManager#ACTION_SHOW_VOICEMAIL_NOTIFICATION}, and forwards to {@link
 * DefaultVoicemailNotifier}. Will ignore the notification if the account has visual voicemail.
 * Legacy voicemail is the traditional, non-visual, dial-in voicemail.
 */
@TargetApi(VERSION_CODES.O)
public class LegacyVoicemailNotificationReceiver extends BroadcastReceiver {

  @Override
  public void onReceive(Context context, Intent intent) {
    LogUtil.i(
        "LegacyVoicemailNotificationReceiver.onReceive", "received legacy voicemail notification");
    Assert.checkArgument(BuildCompat.isAtLeastO());

    PhoneAccountHandle phoneAccountHandle =
        intent.getParcelableExtra(TelephonyManager.EXTRA_PHONE_ACCOUNT_HANDLE);
    if (phoneAccountHandle == null) {
      // TODO: assert instead after API has landed.
      phoneAccountHandle =
          context
              .getSystemService(TelecomManager.class)
              .getDefaultOutgoingPhoneAccount(PhoneAccount.SCHEME_VOICEMAIL);
    }
    if (VoicemailComponent.get(context)
        .getVoicemailClient()
        .isActivated(context, phoneAccountHandle)) {
      LogUtil.i(
          "LegacyVoicemailNotificationReceiver.onReceive",
          "visual voicemail is activated, ignoring notification");
      return;
    }

    // Missing extra means there are unknown numbers of voicemails.
    int count = intent.getIntExtra(TelephonyManager.EXTRA_NOTIFICATION_COUNT, 1);
    if (count == 0) {
      LogUtil.i("LegacyVoicemailNotificationReceiver.onReceive", "clearing notification");
      DefaultVoicemailNotifier.getInstance(context).cancelLegacyNotification();
      return;
    }

    String voicemailNumber = intent.getStringExtra(TelephonyManager.EXTRA_VOICEMAIL_NUMBER);
    PendingIntent callVoicemailIntent =
        intent.getParcelableExtra(TelephonyManager.EXTRA_CALL_VOICEMAIL_INTENT);
    PendingIntent voicemailSettingIntent =
        intent.getParcelableExtra(TelephonyManager.EXTRA_LAUNCH_VOICEMAIL_SETTINGS_INTENT);

    LogUtil.i("LegacyVoicemailNotificationReceiver.onReceive", "sending notification");
    DefaultVoicemailNotifier.getInstance(context)
        .notifyLegacyVoicemail(
            phoneAccountHandle,
            count,
            voicemailNumber,
            callVoicemailIntent,
            voicemailSettingIntent);
  }
}
