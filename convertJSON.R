#Written by Shafik Quoraishee 
#GNNv2 License 2013

options(echo=FALSE) # if you want see commands in output file
args <- commandArgs(trailingOnly = TRUE)

#include the data library
library("rjson")

#get the market id, passed as the 1st argument
marketid <- as.integer(args[1])
print(c("Current market id: ", marketid))

#take market data url (Default: Cryptsy, to be made into an argument)
sdata = c("http://pubapi.cryptsy.com/api.php?method=singlemarketdata&marketid=",marketid)

#the location of the market data
json_file <- paste(sdata[1],sdata[2],sep='')
data = fromJSON(paste(readLines(json_file),collapse=""))

#the different order categories in the file, de-list and convert to vector
recenttrades = unlist(data$return$markets$ADT$recenttrades)
buyorders = unlist(data$return$markets$ADT$buyorders)
sellorders = unlist(data$return$markets$ADT$sellorders)

#general function to get the order data as a matrix
formatOrderData <- function(orderType,formatType) {
	
	#buy/sell orderd
	if(formatType == 1) {
		price = orderType[seq(1, length(orderType), 3)]
		quantity = orderType[seq(2, length(orderType), 3)]
		total = orderType[seq(3, length(orderType), 3)]
	}

	#recent trade dat
	if(formatType == 2) {
		id = orderType[seq(1, length(orderType), 5)]
		time = orderType[seq(2, length(orderType), 5)]
		price = orderType[seq(3, length(orderType), 5)]
		quantity = orderType[seq(4, length(orderType), 5)]
		total = orderType[seq(5, length(orderType), 5)]
	}

	return(price)
}

#Order data as matrices
buy_order_data <- formatOrderData(buyorders,1)
buy_order_data

sell_order_data <- formatOrderData(sellorders,1)
sell_order_data

recent_trade_data <- formatOrderData(recenttrades,2)
recent_trade_data
