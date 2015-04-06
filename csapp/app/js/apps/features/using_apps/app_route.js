App.app_model = function (app_name) {
	var app_url = "/api/app/load/" + app_name;
	return {
			"app_name": app_name,
			"app_url":  app_url + "?" + Api.API_TOKEN + "=" + CloudOsStorage.getItem('cloudos_session')
		};
};

App.AppRoute = Ember.Route.extend({
	model: function(params) {
		return App.app_model(params.app_name);
	}
});
