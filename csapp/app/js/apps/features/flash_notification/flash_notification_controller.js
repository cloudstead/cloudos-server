App.FlashNotificationController = Ember.ObjectController.extend({
	message: function() {
		return this.get('model');
	}.property('model')
});
