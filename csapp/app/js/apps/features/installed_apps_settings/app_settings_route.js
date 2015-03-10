App.AppSettingsRoute = Ember.Route.extend({
	model: function(){
		return Api.get_config_categories();
	}
});
