param environment string
param location string

resource plan 'Microsoft.Web/serverfarms@2022-03-01' = {
  name: 'ai-agent-plan-${environment}'
  location: location
  sku: {
    name: 'P1v3'
    tier: 'PremiumV3'
    capacity: 1
  }
}

resource app 'Microsoft.Web/sites@2022-03-01' = {
  name: 'ai-agent-api-${environment}'
  location: location
  properties: {
    serverFarmId: plan.id
    httpsOnly: true
  }
}
