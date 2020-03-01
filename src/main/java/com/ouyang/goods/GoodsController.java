package com.ouyang.goods;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GoodsController {

	@Autowired
	private GoodsService goodsService;
	
	@GetMapping("/goods/{goodsName}")
	public Goods queryGoodsByName(@PathVariable("goodsName") String goodsName) {

		return goodsService.queryGoodsByName(goodsName);
		
	}
	
}
