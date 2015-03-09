App.Group = Ember.Object.extend({
	save: function() {
		var group_commited = this._commit_add();

		if (group_commited){
			App.Group.addGroup(this);
		}

		return group_commited;
	},

	updateWith: function(data) {
		var group_commited = this._commit_edit(data);

		if (group_commited){
			this.modifyWith(data);
		}

		return group_commited;
	},

	destroy: function() {
		var group_commited = this._commit_delete();

		if (group_commited){
			App.Group.removeGroup(this);
		}

		return group_commited;
	},

	modifyWith: function(data){
		this.set('name', data.name);
		this.set('recipients', data.recipients);
	},

	_commit_add: function() {
		var group_commited = Api.add_group(
			{
				name: this.name,
				recipients: this.recipients,
				info: this.info
			}
		);

		return group_commited;
	},

	_commit_edit: function(data) {
		var group_commited = Api.edit_group(
			{
				name: data.name,
				recipients: data.recipients
			}
		);

		return group_commited;
	},

	_commit_delete: function() {
		var group_commited = Api.delete_group(this.name);

		return group_commited;
	}
});

App.Group.reopenClass({
	all: Ember.ArrayProxy.create({content: []}),

	findAll: function() {
		var data = Api.get_all_groups();
		App.Group.all.clear();
		data.forEach(function(datum) {
			App.Group.all.pushObject(App.Group.create(datum));
		});
		return App.Group.all;
	},

	findByName: function(group_name) {
		var group_data = Api.find_group(group_name);

		return App.Group.create(
			{
				name: group_data.name,
				recipients: group_data.members
			}
		);
	},

	getByName: function(group_name){
		return App.Group.all.findBy('name', group_name)
	},

	addGroup: function(group) {
		App.Group.all.pushObject(group);
	},

	removeGroup: function(group) {
		App.Group.all.removeObject(group);
	}
});
