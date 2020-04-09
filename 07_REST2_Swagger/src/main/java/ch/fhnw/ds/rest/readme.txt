Quelle:
https://stackoverflow.com/questions/57564837/is-there-any-spring-boot-with-jersey-framework-that-supports-openapi-3-0

Ressourcen:
curl -X GET -H "Accept:application/json" http://localhost:8080/api/users
curl -X GET -H "Accept:application/xml" http://localhost:8080/api/users
curl -X GET -H "Accept:application/xstream" http://localhost:8080/api/users

curl -X GET -H "Accept:application/json" http://localhost:8080/api/users/1
curl -X GET -H "Accept:application/xml" http://localhost:8080/api/users/1
curl -X GET -H "Accept:application/xstream" http://localhost:8080/api/users/1


OpenAPI definition:
http://localhost:8080/api/openapi.json
http://localhost:8080/api/openapi.yaml
http://editor.swagger.io/

Swagger UI:
http://localhost:8080/swagger-ui/index.html?url=http://localhost:8080/api/openapi.json