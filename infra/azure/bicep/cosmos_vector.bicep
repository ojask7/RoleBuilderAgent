param environment string
param location string

resource cosmos 'Microsoft.DocumentDB/databaseAccounts@2023-04-15' = {
  name: 'ai-agent-cosmos-${environment}'
  location: location
  kind: 'GlobalDocumentDB'
  properties: {
    databaseAccountOfferType: 'Standard'
    locations: [
      {
        locationName: location
        failoverPriority: 0
      }
    ]
    capabilities: [
      {
        name: 'EnableVectorSearch'
      }
    ]
  }
}
