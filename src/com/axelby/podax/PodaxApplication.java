package com.axelby.podax;

import android.app.Application;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

@ReportsCrashes(formKey = "",
		formUri = "http://podax.axelby.com/error",
		mode = ReportingInteractionMode.DIALOG,
		resToastText = R.string.crash_toast_text,
		resDialogText = R.string.crash_dialog_text,
		resDialogCommentPrompt = R.string.crash_comment_prompt,
		resDialogOkToast = R.string.crash_dialog_ok_text)
public class PodaxApplication extends Application {

	@Override
	public void onCreate() {
		ACRA.init(this);
		super.onCreate();

		PodaxLog.ensureRemoved(this);
	}

}
