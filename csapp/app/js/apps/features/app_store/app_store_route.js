App.AppstoreRoute = Ember.Route.extend({
	model: function() {
		return App.CloudOsApp.findPaginated(DefaultPagination);
	},

	setupController: function(controller, model) {
		this._super(controller, model);
		if (controller.get('currentPage') > 1){
			controller.set('currentPage', 1);
			this.refresh();
		}
	},

	actions: {
		transitionToConfigApp: function (app) {
			this.transitionTo("config_app", app.name);
		},
		openModal: function(modalName){
			return this.render(modalName, {
				into: 'application',
				outlet: 'modal'
			});
		},
		closeModal: function(){
			this.controllerFor('appstore').set("isHidden", true);
			this.controllerFor('appstore').set('user_message_priority', '');
			this.controllerFor('appstore').set('user_message', 'Intializing download...');
			return this.disconnectOutlet({
				outlet: 'modal',
				parentView: 'application'
			});
		}
	}
});
