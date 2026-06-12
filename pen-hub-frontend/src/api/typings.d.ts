declare namespace API {
  type AgentExecutionStats = {
    taskId?: string
    totalDurationMs?: number
    agentCount?: number
    agentDurations?: Record<string, any>
    overallStatus?: string
    logs?: AgentLog[]
  }

  type AgentLog = {
    id?: number
    taskId?: string
    agentName?: string
    startTime?: string
    endTime?: string
    durationMs?: number
    status?: string
    errorMessage?: string
    prompt?: string
    inputData?: string
    outputData?: string
    createTime?: string
    updateTime?: string
    isDelete?: number
  }

  type ArticleAiModifyOutlineRequest = {
    taskId?: string
    modifySuggestion?: string
  }

  type ArticleConfirmOutlineRequest = {
    taskId?: string
    outline?: OutlineSection[]
  }

  type ArticleConfirmTitleRequest = {
    taskId?: string
    selectedMainTitle?: string
    selectedSubTitle?: string
    userDescription?: string
  }

  type ArticleCreateRequest = {
    topic?: string
    style?: string
    enabledImageMethods?: string[]
    contentType?: string
    platform?: string
    duration?: string
    liveType?: string
    productInfo?: string
    participantCount?: string
  }

  type ArticleQueryRequest = {
    pageNum?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
    userId?: number
    status?: string
  }

  type ArticleVO = {
    id?: number
    taskId?: string
    userId?: number
    topic?: string
    userDescription?: string
    mainTitle?: string
    subTitle?: string
    titleOptions?: TitleOption[]
    outline?: OutlineItem[]
    content?: string
    fullContent?: string
    coverImage?: string
    images?: ImageItem[]
    status?: string
    phase?: string
    errorMessage?: string
    createTime?: string
    completedTime?: string
    reviewScore?: number
    reviewSuggestions?: string[]
    isFavorited?: number
    tags?: string[]
    contentType?: string
    scriptStructure?: string
  }

  type ArticleUpdateContentRequest = {
    taskId?: string
    content?: string
  }

  type BaseResponseAgentExecutionStats = {
    code?: number
    data?: AgentExecutionStats
    message?: string
  }

  type BaseResponseArticleVO = {
    code?: number
    data?: ArticleVO
    message?: string
  }

  type BaseResponseBoolean = {
    code?: number
    data?: boolean
    message?: string
  }

  type BaseResponseListString = {
    code?: number
    data?: string[]
    message?: string
  }

  type BaseResponseListOutlineSection = {
    code?: number
    data?: OutlineSection[]
    message?: string
  }

  type BaseResponseListPaymentRecord = {
    code?: number
    data?: PaymentRecord[]
    message?: string
  }

  type BaseResponseListRedemptionCodeVO = {
    code?: number
    data?: RedemptionCodeVO[]
    message?: string
  }

  type BaseResponseListRedemptionRecordVO = {
    code?: number
    data?: RedemptionRecordVO[]
    message?: string
  }

  type BaseResponseLoginUserVO = {
    code?: number
    data?: LoginUserVO
    message?: string
  }

  type BaseResponseLong = {
    code?: number
    data?: number
    message?: string
  }

  type BaseResponseMapStringObject = {
    code?: number
    data?: Record<string, any>
    message?: string
  }

  type BaseResponsePageArticleVO = {
    code?: number
    data?: PageArticleVO
    message?: string
  }

  type BaseResponsePageUserVO = {
    code?: number
    data?: PageUserVO
    message?: string
  }

  type BaseResponseRedemptionRecordVO = {
    code?: number
    data?: RedemptionRecordVO
    message?: string
  }

  type BaseResponseStatisticsVO = {
    code?: number
    data?: StatisticsVO
    message?: string
  }

  type BaseResponseString = {
    code?: number
    data?: string
    message?: string
  }

  type BaseResponseUser = {
    code?: number
    data?: User
    message?: string
  }

  type BaseResponseUserVO = {
    code?: number
    data?: UserVO
    message?: string
  }

  type BaseResponseVoid = {
    code?: number
    data?: Record<string, any>
    message?: string
  }

  type DeleteRequest = {
    id?: number
  }

  type getArticleParams = {
    taskId: string
  }

  type getExecutionLogsParams = {
    taskId: string
  }

  type getProgressParams = {
    taskId: string
  }

  type getUserByIdParams = {
    id: number
  }

  type getUserVOByIdParams = {
    id: number
  }

  type ImageItem = {
    position?: number
    url?: string
    method?: string
    keywords?: string
    sectionTitle?: string
    description?: string
  }

  type LoginUserVO = {
    id?: number
    userAccount?: string
    userNickname?: string
    userAvatar?: string
    userProfile?: string
    userRole?: string
    quota?: number
    vipLevel?: number
    createTime?: string
    updateTime?: string
  }

  type OutlineItem = {
    section?: number
    title?: string
    points?: string[]
  }

  type OutlineSection = {
    section?: number
    title?: string
    points?: string[]
  }

  type PageArticleVO = {
    records?: ArticleVO[]
    pageNumber?: number
    pageSize?: number
    totalPage?: number
    totalRow?: number
    optimizeCountQuery?: boolean
  }

  type PageUserVO = {
    records?: UserVO[]
    pageNumber?: number
    pageSize?: number
    totalPage?: number
    totalRow?: number
    optimizeCountQuery?: boolean
  }

  type PaymentRecord = {
    id?: number
    userId?: number
    stripeSessionId?: string
    stripePaymentIntentId?: string
    amount?: number
    currency?: string
    status?: string
    productType?: string
    description?: string
    refundTime?: string
    refundReason?: string
    createTime?: string
    updateTime?: string
  }

  type RedemptionCodeCreateRequest = {
    productType?: string
    count?: number
    description?: string
  }

  type RedemptionCodeVO = {
    id?: number
    code?: string
    productType?: string
    productDescription?: string
    maxUses?: number
    usedCount?: number
    status?: string
    statusDescription?: string
    expireTime?: string
    description?: string
    createTime?: string
  }

  type RedemptionRecordVO = {
    id?: number
    userId?: number
    code?: string
    productType?: string
    productDescription?: string
    status?: string
    statusDescription?: string
    expireTime?: string
    description?: string
    createTime?: string
  }

  type RedemptionRequest = {
    code?: string
  }

  type refundParams = {
    reason?: string
  }

  type SseEmitter = {
    timeout?: number
  }

  type StatisticsVO = {
    todayCount?: number
    weekCount?: number
    monthCount?: number
    totalCount?: number
    successRate?: number
    avgDurationMs?: number
    activeUserCount?: number
    totalUserCount?: number
    vipUserCount?: number
    quotaUsed?: number
  }

  type TitleOption = {
    mainTitle?: string
    subTitle?: string
  }

  type User = {
    id?: number
    userAccount?: string
    userPassword?: string
    userNickname?: string
    userAvatar?: string
    userProfile?: string
    userRole?: string
    quota?: number
    vipTime?: string
    vipType?: string
    editTime?: string
    createTime?: string
    updateTime?: string
    isDelete?: number
  }

  type UserAddRequest = {
    userName?: string
    userAccount?: string
    userAvatar?: string
    userProfile?: string
    userRole?: string
  }

  type UserLoginRequest = {
    userAccount?: string
    userPassword?: string
  }

  type UserQueryRequest = {
    pageNum?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
    id?: number
    userName?: string
    userAccount?: string
    userProfile?: string
    userRole?: string
  }

  type UserRegisterRequest = {
    userAccount?: string
    userPassword?: string
    checkPassword?: string
  }

  type UserUpdateRequest = {
    id?: number
    userName?: string
    userAvatar?: string
    userProfile?: string
    userRole?: string
  }

  type UserUpdateProfileRequest = {
    userNickname?: string
    userAvatar?: string
    userProfile?: string
  }

  type UserChangePasswordRequest = {
    oldPassword?: string
    newPassword?: string
    checkNewPassword?: string
  }

  type UserVO = {
    id?: number
    userAccount?: string
    userNickname?: string
    userAvatar?: string
    userProfile?: string
    userRole?: string
    createTime?: string
  }

  type Template = {
    id?: number
    name?: string
    category?: string
    platform?: string
    style?: string
    topicExample?: string
    recommendedImageMethods?: string
    description?: string
    sortOrder?: number
    status?: number
    createTime?: string
    updateTime?: string
    isDelete?: number
  }

  type BaseResponsePageTemplate = {
    code?: number
    data?: PageTemplate
    message?: string
  }

  type PageTemplate = {
    records?: Template[]
    pageNumber?: number
    pageSize?: number
    totalRow?: number
    totalPage?: number
  }

  type BaseResponseTemplate = {
    code?: number
    data?: Template
    message?: string
  }

  type ArticleFeedback = {
    id?: number
    articleId?: number
    userId?: number
    rating?: number
    comment?: string
    createTime?: string
  }

  type BaseResponseArticleFeedback = {
    code?: number
    data?: ArticleFeedback
    message?: string
  }
}
