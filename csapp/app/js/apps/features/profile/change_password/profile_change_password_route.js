App.ProfileChangePasswordRoute = Ember.Route.extend({
	model: function () {
		return this.modelFor('profile');
	},
	renderTemplate: function() {
		this.render('change_password_modal', { outlet: 'change_password', controller: this.controller });
	}
});
