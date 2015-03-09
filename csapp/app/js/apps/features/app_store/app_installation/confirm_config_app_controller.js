App.ConfirmConfigAppController = App.AppStoreConfigBasicController.extend({

	hiddenClass: function() {
		return this.get("isInstallModalCloseHidden") ? "hide" : "";
	}.property("isInstallModalCloseHidden"),

	actions: {
		doInstall: function() {
			this.send("goToInstall", this);
		},

		doConfig: function() {
			this.send("transitionToConfig");
		},
	}
});
