App.AppSettingsRoute = CloudOSProtectedRoute.extend({
	model: function(){
		return Api.get_config_categories();
	}
});
