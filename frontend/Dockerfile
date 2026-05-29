FROM node:22-alpine

WORKDIR /app

COPY frontend/package*.json ./
RUN --mount=type=cache,id=rivalcode-npm-cache,target=/root/.npm,sharing=locked npm ci --no-audit --no-fund

COPY frontend .

EXPOSE 3000

CMD ["npm", "run", "dev"]
