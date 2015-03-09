App.EappController = Ember.ObjectController.extend({
	hasTaskbarIcon: function(){
		var assets = this.get('assets');
		return !Ember.isNone(assets) &&
			(!Ember.isNone(assets.taskbarIconUrl) || !Ember.isEmpty(assets.taskbarIconUrl));
	}.property(),

	taskbarIconCaption: function(){
		var text = this.get('assets.taskbarIconAltText');
		return text === undefined ? this.get('name') : text;
	}.property()
});
