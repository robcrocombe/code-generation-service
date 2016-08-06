FORMAT: 1A
HOST: http://localhost:8080/service

# Code Generation API

Documentation for the Code Generation Service.

## Group Generators

## Get all generators [/generators]

### /generators [GET]
Get the information of all generators.

+ Response 200 (application/json)

    + Body

			[{
				name: "github_owner/repo_name",
				description: "Description of the generator"
				properties:
				[
					{
						name: "Property name",
						value: "The default value or null",
						description: "Property description"
					}
				]
			}]

## Get a single generator [/generators/{owner}/{repo}{?raw}]

### /generators [GET]
Get the information of a single generator.

+ Parameters

	+ owner: `robcrocombe` (required, string) - The name of the GitHub user who owns the respository.
	+ repo: `cgs-emf-gen` (required, string) - The name of the GitHub repository.
	+ raw: `` (optional, boolean) - Return the build.xml Ant file for your own processing.
		+ Default: `false`

+ Response 200 (application/json)

	The default response, or if *raw=false*.

    + Body

			{
				name: "github_owner/repo_name",
				description: "Description of the generator"
				properties:
				[
					{
						name: "Property name",
						value: "The default value or null",
						description: "Property description"
					}
				]
			}

+ Response 200 (application/xml)

	The response if *raw=true*.

    + Body

			<project name="cgs-emf-gen" default="main">
				<property name="model" value="" description="The input EMF model"/>

				<target name="loadModels">
				    <epsilon.emf.register file="test.ecore"/>
				    <epsilon.emf.loadModel name="M" modelfile="${model}" metamodeluri="http://cs.york.ac.uk/" read="true" store="true"/>
				</target>

				<target name="main" depends="loadModels">
					<epsilon.egl src="test.egx" outputroot="${outputRoot}">
						<model ref="M"/>
					</epsilon.egl>
				</target>
			</project>

+ Response 404 (application/json)

	+ Body

			{
				error: "Exception message"
			}

## Publish a generator [/generators{?owner,repo}]

### /generators [POST]
Publish a generator to the service so it can be used in a job.

+ Parameters

	+ owner: `robcrocombe` (required, string) - The name of the GitHub user who owns the respository.
	+ repo: `cgs-emf-gen` (required, string) - The name of the GitHub repository to upload.

+ Response 201

+ Response 400 (application/json)

	+ Body

			{
				error: "The user has made a bad request, and the reason will be given here."
			}

+ Response 500 (application/json)

	+ Body

			{
				error: "There was an error processing the GitHub repo and the reason will be given here."
			}

## Group Jobs

## Get job status [/job{?id}]

### /job [GET]
Get the status of a job, or if it's completed, the zip containing the generated files.

+ Parameters

	+ id: `12345` (required, string) - The job ID given when a successful POST to /job is made.

+ Response 200 (application/zip)

+ Response 202 (text/plain)

	+ Body

			"A message describing the current stage of the process."

+ Response 404 (application/json)

	+ Body

			{
				error: "A job with that ID was not found."
			}

+ Response 499 (text/plain)

	+ Body

			"Job cancelled by user."

+ Response 500 (application/json)

	+ Body

			{
				error: "The error message descibing what went wrong processing the request."
			}

## Post a new job [/job]

### /job [POST]
Submit your models and properties associated with a published generator.

+ Request (multipart/form-data)

	+ Body

			- manifest (required, application/json)
				{
					githubOwner: "",
					repoName: "",
					properties:
					[
						{
							name: "",
							value: ""
						}
					],
					files:
					[
						{
							fieldName: "",
							propertyName: "",
							filePath: ""
						}
					]
				}
			- file1 (required, application/octet-stream)
			- file2 (optional)...

+ Response 202

	Returns a job ID that is used to retrieve the status of the job and the completed files.

    + Body

			123456

+ Response 400 (application/json)

	+ Body

			{
				error: "The user has made a bad request, and the reason will be given here."
			}

+ Response 500 (application/json)

	+ Body

			{
				error: "The error message descibing what went wrong processing the request."
			}

## Abort a processing job [/job{?id}]

### /job [DELETE]
Abort a job that is currently in process.

+ Parameters

	+ id: `12345` (required, string) - The job ID given when a successful POST to /job is made.

+ Response 200

+ Response 304

+ Response 404 (application/json)

	+ Body

			{
				error: "A job with that ID was not found."
			}

+ Response 409

	Conflict

	+ Body

			{
				error: "The job completed successfully and therefore was not cancelled."
			}
