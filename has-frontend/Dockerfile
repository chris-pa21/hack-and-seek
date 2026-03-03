# build stage
FROM node:20-alpine AS build
WORKDIR /frontend
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

# run stage
FROM nginx:stable-alpine
COPY --from=build /frontend/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
