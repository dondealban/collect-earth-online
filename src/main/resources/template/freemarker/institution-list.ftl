<div id="institution-list" ng-controller="InstitutionListController as institutionList" ng-init="institutionList.initialize()">
    <h1>Institutions [{{ institutionList.institutionList.length }}]</h1>
    <ul>
        <#if role?? && role == "admin">
        <li><a class="create-institution" href="institution">Create New Institution</a></li>
        </#if>
        <li ng-repeat="institution in institutionList.institutionList">
            <a href="institution?id={{ institution.id }}">{{ institution.name }}</a>
        </li>
    </ul>
</div>
