App.SelectedappController = Ember.ObjectController.extend({
	actions: {
		select_app: function (app_name) {
			window.location.replace('/#/app/' + app_name);
		},

		install_app: function (app_name) {
		}
	}
});
