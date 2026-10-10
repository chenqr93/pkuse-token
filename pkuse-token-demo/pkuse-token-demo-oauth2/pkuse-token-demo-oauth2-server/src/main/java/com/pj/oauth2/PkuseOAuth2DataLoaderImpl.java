package com.pj.oauth2;

import com.bdsoft.pkusetoken.oauth2.data.loader.PkuseOAuth2DataLoader;
import com.bdsoft.pkusetoken.oauth2.data.model.loader.PkuseClientModel;
import com.pj.mock.PkuseClientMockDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Sa-Token OAuth2：自定义数据加载器
 *
 * @author click33
 */
@Component
public class PkuseOAuth2DataLoaderImpl implements PkuseOAuth2DataLoader {

	@Autowired
	PkuseClientMockDao saClientMockDao;

	// 根据 clientId 获取 Client 信息
	@Override
	public PkuseClientModel getClientModel(String clientId) {
		// 此为模拟数据，真实环境需要从数据库查询
		return saClientMockDao.getClientModel(clientId);
	}
	
	// 根据 clientId 和 loginId 获取 openid
	@Override
	public String getOpenid(String clientId, Object loginId) {
		// 此处使用框架默认算法生成 openid，真实项目建议改为从数据库查询
		return PkuseOAuth2DataLoader.super.getOpenid(clientId, loginId);
	}

}
