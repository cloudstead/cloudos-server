App.SelectedappRoute = Ember.Route.extend({
	model: function(params) {
		return App.CloudOsApp.all.findBy('name', params.appname);
	},

	afterModel: function(model, transition) {
		if (Ember.isNone(model)){
			this.transitionTo('appstore');
		}
	}
});
