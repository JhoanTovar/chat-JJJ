# Chat Application with PostgreSQL Persistence

A WhatsApp-style chat application with TCP/UDP communication and PostgreSQL database persistence.

## Features

- User registration and authentication
- Private messaging
- Group messaging
- Voice notes (audio messages)
- Voice calls (UDP)
- **PostgreSQL database persistence** for:
  - Users
  - Messages (private and group)
  - Groups and memberships
  - Call history

## Database Setup

### Prerequisites

- PostgreSQL 12 or higher
- Java 17 or higher
- Gradle

### Database Configuration

1. **Create the database:**

\`\`\`bash
createdb chatdb
\`\`\`

2. **Configure connection (optional):**

The application will automatically create the schema on first run. You can configure the database connection using environment variables:

\`\`\`bash
export DATABASE_URL="postgres://username:password@localhost:5432/chatdb"
# OR
export DB_USER="your_username"
export DB_PASSWORD="your_password"
\`\`\`

Default configuration (if no environment variables are set):
- Host: localhost
- Port: 5432
- Database: chatdb
- User: postgres
- Password: postgres

### Database Schema

The application automatically creates the following tables:

- **users**: User accounts with online status
- **groups**: Chat groups
- **group_members**: Group membership (many-to-many)
- **messages**: All messages (private and group)
- **calls**: Call history with duration and status

## Running the Application

### Build the project:

\`\`\`bash
./gradlew build
\`\`\`

### Run the server:

\`\`\`bash
./gradlew runServer
\`\`\`

### Run a client:

\`\`\`bash
./gradlew runClient
\`\`\`

### Run voice server (for calls):

\`\`\`bash
./gradlew runVoiceServer
\`\`\`

## Usage

1. Start the server first
2. Launch one or more clients
3. Register or login with a username
4. Use the menu to:
   - Send private messages
   - Create and join groups
   - Send group messages
   - Make voice calls
   - Send voice notes
   - View message history (persisted in database)

## Data Persistence

All data is now persisted in PostgreSQL:

- **User accounts** are saved and can be reused across sessions
- **Message history** is permanently stored and can be retrieved
- **Group information** and memberships are maintained
- **Call logs** track all calls with duration and status

## Architecture

- **TCP**: Used for text messages, commands, and signaling
- **UDP**: Used for real-time voice data during calls
- **PostgreSQL**: Persistent storage for all application data
- **HikariCP**: Connection pooling for optimal database performance

## Environment Variables

- `DATABASE_URL`: Full PostgreSQL connection URL
- `DB_USER`: Database username (default: postgres)
- `DB_PASSWORD`: Database password (default: postgres)
