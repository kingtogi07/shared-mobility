# order
cd Order
mvn package
docker build -t 879772956301.dkr.ecr.ap-northeast-2.amazonaws.com/user01-order:latest .
docker push 879772956301.dkr.ecr.ap-northeast-2.amazonaws.com/user01-order:latest

# payment
cd ..
cd Payment
mvn package
docker build -t 879772956301.dkr.ecr.ap-northeast-2.amazonaws.com/user01-payment:latest .
docker push 879772956301.dkr.ecr.ap-northeast-2.amazonaws.com/user01-payment:latest

# rent
cd ..
cd Rent
mvn package
docker build -t 879772956301.dkr.ecr.ap-northeast-2.amazonaws.com/user01-rent:latest .
docker push 879772956301.dkr.ecr.ap-northeast-2.amazonaws.com/user01-rent:latest

# stock
cd ..
cd Stock
mvn package
docker build -t 879772956301.dkr.ecr.ap-northeast-2.amazonaws.com/user01-stock:latest .
docker push 879772956301.dkr.ecr.ap-northeast-2.amazonaws.com/user01-stock:latest

# sender
cd ..
cd Sender
mvn package
docker build -t 879772956301.dkr.ecr.ap-northeast-2.amazonaws.com/user01-sender:latest .
docker push 879772956301.dkr.ecr.ap-northeast-2.amazonaws.com/user01-sender:latest

# dashboard
cd ..
cd Dashboard
mvn package
docker build -t 879772956301.dkr.ecr.ap-northeast-2.amazonaws.com/user01-dashboard:latest .
docker push 879772956301.dkr.ecr.ap-northeast-2.amazonaws.com/user01-dashboard:latest

# gateway
cd ..
cd gateway
mvn package
docker build -t 879772956301.dkr.ecr.ap-northeast-2.amazonaws.com/user01-gateway:latest .
docker push 879772956301.dkr.ecr.ap-northeast-2.amazonaws.com/user01-gateway:latest
