App.ResetPasswordRoute = Ember.Route.extend({
	model: function (params) {
		return { token : params['token'] };
	}
});
