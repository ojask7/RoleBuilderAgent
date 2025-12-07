param environment string = 'dev'
param location string = resourceGroup().location

module openAiModule 'openai.bicep' = {
  name: 'openai-${environment}'
  params: {
    location: location
    environment: environment
  }
}

module appServiceModule 'appservice.bicep' = {
  name: 'appservice-${environment}'
  params: {
    location: location
    environment: environment
  }
}

module cosmosModule 'cosmos_vector.bicep' = {
  name: 'cosmos-${environment}'
  params: {
    location: location
    environment: environment
  }
}
