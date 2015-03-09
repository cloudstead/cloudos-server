App.ConfigAppController = App.AppStoreConfigBasicController.extend({

	actions: {
		doInstall: function() {
			Api.write_app_config(this.get("app"), this.get("arrangedContent"));
			this.send("transitionToConfirm");
		},

		checkForConfig: function() {
			if (Ember.isEmpty(this.get("arrangedContent"))){
				this.send("goToInstall", this);
			}
		}
	}
});
