module.exports = function(lineman) {

	get_feature = function(feature_name) {
		return "app/js/apps/features/" + feature_name + "/**/*.js";
	}

	//Override application configuration here. Common examples follow in the comments.
	return {

		removeTasks:{
			common: ["jshint","handlebars"],
		},
		appendTasks:{
			dist: ["concat_sourcemap:gen_apps","copy:local","copy:csapp_gen","copy:csapp_dist","copy:build"],
		},
		concat_sourcemap: {
			options:
			{
				sourcesContent: true
			},
			gen_apps: {
				files: {
					"generated/js/apps/cloudos-app.js": [
						"app/js/apps/cloudos-base.js",
						get_feature("login"),
						get_feature("logout"),
						get_feature("settings"),
						get_feature("profile"),
						get_feature("reset_password"),
						get_feature("two_factor_verification"),
						get_feature("request_message"),
						get_feature("using_apps")
					],
					"generated/js/apps/admin-app.js": [
						"app/js/apps/admin-base.js",
						get_feature("logout"),
						get_feature("request_message"),
						get_feature("using_apps"),
						get_feature("app_store"),
						get_feature("accounts"),
						get_feature("email_domains"),
						get_feature("aliases"),
						get_feature("ssl_certificates"),
						get_feature("groups"),
						get_feature("valet_keys"),
						get_feature("installed_apps_settings"),
						get_feature("task_progress"),
					]
				}
			}
		},
		copy: {
			local: {
				files:[
						{	expand: true,
							cwd: 'app/js/local/',
							src: '**/*',
							dest: 'dist/js/local',
							}
				]
			},
			csapp_dist: {
				files:[
						{	expand: true,
							cwd: 'generated/js/apps',
							src: '*app*',
							dest: 'dist/js/apps',
							}
				]
			},
			csapp_gen: {
				files:[
						{	expand: true,
							cwd: 'app/js/apps/',
							src: '*app*',
							dest: 'generated/js/apps',
							}
				]
			},
			build: {
				files:[
						{	expand: true,
							cwd: 'dist',
							src: '**/*',
							dest: '../target/classes/static',
							}
				]
			}
		}

	};
};
